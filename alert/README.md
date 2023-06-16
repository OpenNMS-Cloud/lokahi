# TODO

## Pagination support

Implement proper request filtering and pagination support.

See `org.opennms.horizon.alertservice.grpc.AlertGrpcService#listAlerts`

## Modeling and API contract for the "alert definitions

See `org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository` and `org.opennms.horizon.alertservice.grpc.AlertConfigurationGrpcService`.

## Scalable implementation of the "alert engine"

See `org.opennms.horizon.alertservice.service.AlertEngine`
