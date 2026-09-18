DO $$
BEGIN
    IF to_regclass('public.warehouse_order') IS NOT NULL THEN
        ALTER TABLE warehouse_order
            ADD COLUMN IF NOT EXISTS assigned_warehouse_id BIGINT;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_warehouse_order_assigned_warehouse'
        ) THEN
            ALTER TABLE warehouse_order
                ADD CONSTRAINT fk_warehouse_order_assigned_warehouse
                FOREIGN KEY (assigned_warehouse_id) REFERENCES auth_user(id)
                ON DELETE SET NULL;
        END IF;

        CREATE INDEX IF NOT EXISTS idx_warehouse_order_assigned_warehouse
            ON warehouse_order(assigned_warehouse_id);
    END IF;
END $$;
