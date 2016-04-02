CREATE TABLE credentials (
  service_id VARCHAR(36) PRIMARY KEY,
  username   VARCHAR(36),
  password   VARCHAR(36)
);

CREATE TABLE plan (
  service_id    VARCHAR(36) PRIMARY KEY,
  expire_second INT,
  maximum_size INT,
  FOREIGN KEY (service_id) REFERENCES credentials(service_id)
);