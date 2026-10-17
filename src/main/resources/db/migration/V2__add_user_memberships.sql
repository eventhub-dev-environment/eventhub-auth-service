CREATE TABLE user_global_roles (
    user_id varchar(255) NOT NULL,
    global_role varchar(255) NOT NULL,
    CONSTRAINT pk_user_global_roles PRIMARY KEY (user_id, global_role),
    CONSTRAINT fk_user_global_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_tenant_roles (
    user_id varchar(255) NOT NULL,
    tenant_id varchar(255) NOT NULL,
    tenant_role varchar(255) NOT NULL,
    CONSTRAINT pk_user_tenant_roles PRIMARY KEY (user_id, tenant_id, tenant_role),
    CONSTRAINT fk_user_tenant_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);
