package com.helishop.core.modules.product.service;

import java.util.Set;

public record ProductStockChanged(Set<Long> productIds) {}
