CREATE TABLE provinces(
    id BIGSERIAL PRIMARY KEY,          -- int8, auto increment
    code INTEGER,                       -- int4
    name TEXT NOT NULL,
    code_name TEXT,
    division_type TEXT,
    phone_code INTEGER,                -- int4
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_updated_at
BEFORE UPDATE ON provinces
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();