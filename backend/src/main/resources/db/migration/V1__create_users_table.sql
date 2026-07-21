CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       username VARCHAR(40) NOT NULL,
                       first_name VARCHAR(80) NOT NULL,
                       last_name VARCHAR(80) NOT NULL,
                       email VARCHAR(160) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       phone_number VARCHAR(30) NOT NULL,
                       date_of_birth DATE NOT NULL,
                       city VARCHAR(80) NOT NULL,
                       state_region VARCHAR(80) NOT NULL,
                       country VARCHAR(80) NOT NULL,
                       role VARCHAR(30) NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT uk_users_username UNIQUE (username),
                       CONSTRAINT uk_users_phone_number UNIQUE (phone_number)
);