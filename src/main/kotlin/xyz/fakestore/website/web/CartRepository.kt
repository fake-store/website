package xyz.fakestore.website.web

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
class CartRepository(private val dsl: DSLContext) {

    fun getOrCreateCartId(userId: UUID): UUID {
        val existing = dsl.select(DSL.field("id", UUID::class.java))
            .from(DSL.table("cart"))
            .where(DSL.field("user_id", UUID::class.java).eq(userId))
            .fetchOne(DSL.field("id", UUID::class.java))
        if (existing != null) return existing

        return dsl.insertInto(DSL.table("cart"))
            .columns(DSL.field("user_id", UUID::class.java))
            .values(userId)
            .returning(DSL.field("id", UUID::class.java))
            .fetchOne(DSL.field("id", UUID::class.java))!!
    }

    fun getItems(userId: UUID): List<CartItem> {
        return dsl.select(
                DSL.field("cart_items.product_id", UUID::class.java),
                DSL.field("cart_items.title", String::class.java),
                DSL.field("cart_items.price", BigDecimal::class.java),
                DSL.field("cart_items.quantity", Int::class.java)
            )
            .from(DSL.table("cart_items"))
            .join(DSL.table("cart")).on(
                DSL.field("cart_items.cart_id", UUID::class.java)
                    .eq(DSL.field("cart.id", UUID::class.java))
            )
            .where(DSL.field("cart.user_id", UUID::class.java).eq(userId))
            .fetch { r ->
                CartItem(
                    r.get(DSL.field("cart_items.product_id", UUID::class.java)),
                    r.get(DSL.field("cart_items.title", String::class.java)),
                    r.get(DSL.field("cart_items.price", BigDecimal::class.java)),
                    r.get(DSL.field("cart_items.quantity", Int::class.java))
                )
            }
    }

    fun upsertItem(userId: UUID, item: CartItem) {
        val cartId = getOrCreateCartId(userId)
        dsl.insertInto(DSL.table("cart_items"))
            .columns(
                DSL.field("cart_id", UUID::class.java),
                DSL.field("product_id", UUID::class.java),
                DSL.field("title", String::class.java),
                DSL.field("price", BigDecimal::class.java),
                DSL.field("quantity", Int::class.java)
            )
            .values(cartId, item.productId, item.title, item.price, item.quantity)
            .onConflict(DSL.field("cart_id"), DSL.field("product_id"))
            .doUpdate()
            .set(
                DSL.field("quantity", Int::class.java),
                DSL.field("quantity", Int::class.java).plus(item.quantity)
            )
            .execute()
    }

    fun removeItem(userId: UUID, productId: UUID) {
        val cartId = dsl.select(DSL.field("id", UUID::class.java))
            .from(DSL.table("cart"))
            .where(DSL.field("user_id", UUID::class.java).eq(userId))
            .fetchOne(DSL.field("id", UUID::class.java)) ?: return
        dsl.deleteFrom(DSL.table("cart_items"))
            .where(
                DSL.field("cart_id", UUID::class.java).eq(cartId)
                    .and(DSL.field("product_id", UUID::class.java).eq(productId))
            )
            .execute()
    }

    fun clearCart(userId: UUID) {
        val cartId = dsl.select(DSL.field("id", UUID::class.java))
            .from(DSL.table("cart"))
            .where(DSL.field("user_id", UUID::class.java).eq(userId))
            .fetchOne(DSL.field("id", UUID::class.java)) ?: return
        dsl.deleteFrom(DSL.table("cart_items"))
            .where(DSL.field("cart_id", UUID::class.java).eq(cartId))
            .execute()
    }
}
