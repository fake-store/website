package xyz.fakestore.website.web

import java.math.BigDecimal
import java.util.UUID

data class CartItem(
    val productId: UUID,
    val title: String,
    val price: BigDecimal,
    var quantity: Int = 1
)
