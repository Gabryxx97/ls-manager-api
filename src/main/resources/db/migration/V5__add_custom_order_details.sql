DO $$
BEGIN
    IF to_regclass('public.order_detail') IS NOT NULL THEN
        ALTER TABLE order_detail
            ALTER COLUMN article_id DROP NOT NULL,
            ADD COLUMN IF NOT EXISTS custom_description TEXT,
            ADD COLUMN IF NOT EXISTS custom_sku VARCHAR(64),
            ADD COLUMN IF NOT EXISTS custom_unit_of_measure VARCHAR(16);

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'ck_order_detail_article_source'
        ) THEN
            ALTER TABLE order_detail
                ADD CONSTRAINT ck_order_detail_article_source CHECK (
                    (article_id IS NOT NULL
                        AND custom_description IS NULL
                        AND custom_sku IS NULL
                        AND custom_unit_of_measure IS NULL)
                    OR
                    (article_id IS NULL
                        AND custom_description IS NOT NULL
                        AND unit_price IS NOT NULL)
                );
        END IF;
    END IF;
END $$;
