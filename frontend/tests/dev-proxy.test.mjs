import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer as createBackend } from 'node:http';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { createServer } from 'vite';

test('Vite proxies API, same-origin POST and uploads to the configured backend port', async () => {
  const image = Buffer.from([137,80,78,71]);
  const backend = createBackend(async (req,res) => {
    if (req.headers.origin && new URL(req.headers.origin).host !== req.headers.host) {
      res.writeHead(403); res.end('Invalid CORS request'); return;
    }
    if (req.url === '/uploads/proxy-check.png') {
      res.writeHead(200,{'Content-Type':'image/png'}); res.end(image); return;
    }
    const chunks = [];
    for await (const chunk of req) chunks.push(chunk);
    res.writeHead(200,{'Content-Type':'application/json'});
    res.end(JSON.stringify({path:req.url,host:req.headers.host,body:Buffer.concat(chunks).toString()}));
  });
  await new Promise(resolve => backend.listen(0,'127.0.0.1',resolve));
  const previous = process.env.DEV_API_PROXY_TARGET;
  process.env.DEV_API_PROXY_TARGET = `http://127.0.0.1:${backend.address().port}`;
  let vite;
  try {
    const root = fileURLToPath(new URL('../',import.meta.url));
    vite = await createServer({root,configFile:path.join(root,'vite.config.ts'),mode:'test',logLevel:'silent',
      cacheDir:path.join(root,'node_modules','.vite-proxy-test'),optimizeDeps:{noDiscovery:true,include:[]},server:{host:'127.0.0.1',port:0}});
    await vite.listen();
    const origin = `http://127.0.0.1:${vite.httpServer.address().port}`;
    const catalog = await fetch(`${origin}/api/v1/products?page=1&size=1`);
    assert.equal(catalog.status,200);
    assert.equal((await catalog.json()).path,'/api/v1/products?page=1&size=1');
    const login = await fetch(`${origin}/api/v1/auth/login`,{method:'POST',headers:{Origin:origin,'Content-Type':'application/json'},body:'{"probe":true}'});
    assert.equal(login.status,200);
    const posted = await login.json();
    assert.equal(posted.host,new URL(origin).host);
    assert.equal(posted.body,'{"probe":true}');
    const upload = await fetch(`${origin}/uploads/proxy-check.png`);
    assert.equal(upload.status,200); assert.equal(upload.headers.get('content-type'),'image/png');
    assert.deepEqual(Buffer.from(await upload.arrayBuffer()),image);
  } finally {
    await vite?.close();
    backend.closeAllConnections();
    await new Promise(resolve => backend.close(resolve));
    if(previous === undefined) delete process.env.DEV_API_PROXY_TARGET;
    else process.env.DEV_API_PROXY_TARGET = previous;
  }
});
