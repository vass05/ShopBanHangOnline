# ==============================================================================
# HeliShop Core E-Commerce Frontend - Multi-stage Production Dockerfile
# Stage 1: Build static bundle with Node.js 20 Alpine & Vite
# Stage 2: High performance production web server with Nginx 1.27 Alpine
# ==============================================================================

# Stage 1: Builder
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency manifests first for layer caching
COPY package*.json ./

# Install clean dependencies
RUN npm ci

# Copy source code and build production distribution
COPY . .
RUN npm run build

# ==============================================================================
# Stage 2: Production Nginx Server
# ==============================================================================
FROM nginx:1.27-alpine AS runner

# Remove default nginx website
RUN rm -rf /usr/share/nginx/html/*

# Copy built production assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration (SPA routing, Gzip, reverse proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose standard HTTP port
EXPOSE 80

# Container Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:80/ || exit 1

# Run Nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
