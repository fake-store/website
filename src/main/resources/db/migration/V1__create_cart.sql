CREATE TABLE cart (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cart_items (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id    UUID           NOT NULL REFERENCES cart(id) ON DELETE CASCADE,
    product_id UUID           NOT NULL,
    title      VARCHAR(255)   NOT NULL,
    price      NUMERIC(10,2)  NOT NULL,
    quantity   INT            NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    UNIQUE (cart_id, product_id)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE cart, cart_items TO fakestore_orders_user;
