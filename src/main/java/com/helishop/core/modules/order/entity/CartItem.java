package com.helishop.core.modules.order.entity;

import com.helishop.core.common.base.BaseEntity;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "cart_items",
    indexes = {
        @Index(name = "idx_cart_user_id", columnList = "user_id"),
        @Index(name = "idx_cart_sku_id", columnList = "sku_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSku productSku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
