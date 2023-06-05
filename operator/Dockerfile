#Builder
FROM golang:1.19 as builder

WORKDIR /app

COPY Makefile Makefile

COPY go.mod go.mod
COPY go.sum go.sum

RUN make dependencies

COPY cmd/ cmd/
COPY api/ api/
COPY config/ config/
COPY internal/ internal/

RUN make alpine-build

#Runner
FROM alpine:3.15

WORKDIR /app

COPY --from=builder /app/operator .

COPY charts/lokahi charts/lokahi
COPY charts/lokahi-operator/values.yaml charts/lokahi-operator/values.yaml

COPY config/ config/

ENTRYPOINT ["/app/operator"]
