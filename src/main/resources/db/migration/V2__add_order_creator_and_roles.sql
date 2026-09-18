-- Existing standard users become operators. New installations receive the
-- schema from Hibernate after Flyway has completed its baseline migrations.
DO $$
BEGIN
    IF to_regclass('public.auth_user') IS NOT NULL THEN
        UPDATE auth_user
        SET role = 'OPERATOR_ROLE'
        WHERE role = 'USER_ROLE';
    END IF;

    IF to_regclass('public.warehouse_order') IS NOT NULL THEN
        ALTER TABLE warehouse_order
            ADD COLUMN IF NOT EXISTS created_by_id BIGINT;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_warehouse_order_created_by'
        ) THEN
            ALTER TABLE warehouse_order
                ADD CONSTRAINT fk_warehouse_order_created_by
                FOREIGN KEY (created_by_id) REFERENCES auth_user(id)
                ON DELETE SET NULL;
        END IF;
    END IF;
END $$;
