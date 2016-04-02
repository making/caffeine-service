CREATE TABLE caches (
  service_id    VARCHAR(36) PRIMARY KEY,
  expire_second INT,
  maximum_size  INT
);

CREATE TABLE credentials (
  service_id VARCHAR(36) PRIMARY KEY,
  username   VARCHAR(36),
  password   VARCHAR(36),
  FOREIGN KEY (service_id) REFERENCES caches (service_id)
);