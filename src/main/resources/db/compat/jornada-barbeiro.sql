DO $$
DECLARE regra record;
BEGIN
    -- A unicidade antiga impedia barbeiros diferentes de compartilharem o dia.
    FOR regra IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
        WHERE c.conrelid = 'jornada_barbeiro'::regclass
          AND c.contype = 'u' AND cardinality(c.conkey) = 1 AND a.attname = 'dia_semana'
    LOOP
        EXECUTE format('ALTER TABLE jornada_barbeiro DROP CONSTRAINT %I', regra.conname);
    END LOOP;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'jornada_barbeiro'::regclass AND conname = 'uk_jornada_barbeiro_dia') THEN
        ALTER TABLE jornada_barbeiro ADD CONSTRAINT uk_jornada_barbeiro_dia UNIQUE (barbeiro_id, dia_semana);
    END IF;
END $$;
