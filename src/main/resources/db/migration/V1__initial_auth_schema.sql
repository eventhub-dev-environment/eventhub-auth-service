CREATE TABLE users (
    id varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
    password varchar(255) NOT NULL,
    role varchar(255) NOT NULL,
    enabled boolean NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);
