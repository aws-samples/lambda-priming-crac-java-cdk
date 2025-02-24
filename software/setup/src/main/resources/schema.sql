-- Drop table if it exists
DROP TABLE IF EXISTS UnicornEmployee;

-- Drop sequence if it exists
DROP SEQUENCE IF EXISTS unicorn_employee_seq CASCADE;

-- Create new sequence
CREATE SEQUENCE unicorn_employee_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE UnicornEmployee (
    "EmployeeId" INTEGER DEFAULT nextval('unicorn_employee_seq') PRIMARY KEY,
    "EmployeeName"	VARCHAR(512),
    "Location"	VARCHAR(512),
    "Gender"	VARCHAR(512),
    "DateHired"	VARCHAR(512),
    "ExemptStatus"	VARCHAR(512)
);
