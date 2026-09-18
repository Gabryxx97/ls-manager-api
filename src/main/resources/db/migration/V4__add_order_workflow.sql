DO $$
BEGIN
    IF to_regclass('public.warehouse_order') IS NOT NULL THEN
        ALTER TABLE warehouse_order
            ADD COLUMN IF NOT EXISTS taken_in_charge_at TIMESTAMP WITH TIME ZONE;

        UPDATE warehouse_order
        SET status = CASE
            WHEN status = 'DRAFT' THEN 'PROCESSING'
            WHEN status = 'SHIPPED' THEN 'COMPLETED'
            ELSE status
        END;

        UPDATE warehouse_order
        SET taken_in_charge_at = CURRENT_TIMESTAMP
        WHERE assigned_warehouse_id IS NOT NULL
          AND taken_in_charge_at IS NULL;
    END IF;

    IF to_regclass('public.order_detail') IS NOT NULL THEN
        ALTER TABLE order_detail
            ADD COLUMN IF NOT EXISTS status VARCHAR(32),
            ADD COLUMN IF NOT EXISTS picked_quantity INTEGER;

        UPDATE order_detail detail
        SET status = CASE
                WHEN orders.status = 'COMPLETED' THEN 'COMPLETED'
                ELSE 'TO_PICK'
            END,
            picked_quantity = CASE
                WHEN orders.status = 'COMPLETED' THEN detail.quantity
                ELSE 0
            END
        FROM warehouse_order orders
        WHERE detail.order_id = orders.id
          AND (detail.status IS NULL OR detail.picked_quantity IS NULL);

        ALTER TABLE order_detail ALTER COLUMN status SET NOT NULL;
        ALTER TABLE order_detail ALTER COLUMN picked_quantity SET NOT NULL;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'ck_order_detail_picked_quantity_range'
        ) THEN
            ALTER TABLE order_detail
                ADD CONSTRAINT ck_order_detail_picked_quantity_range
                CHECK (picked_quantity >= 0 AND picked_quantity <= quantity);
        END IF;
    END IF;
END $$;
