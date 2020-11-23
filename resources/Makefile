ES ?= localhost:9200
INDEX ?= ci-builds
.PHONY: create-flaky-index
create-flaky-index:
	@curl -X PUT -H 'Content-Type: application/json' \
		-d "@flaky-mapping.json" \
		"$(ES)/$(INDEX)"

.PHONY: add-flaky-failure
add-flaky-failure:
	@curl -X POST -H 'Content-Type: application/json' \
		-d "@flaky-entry.json" \
		"$(ES)/$(INDEX)/_doc"

.PHONY: get-flaky-failure
get-flaky-failure:
	@curl -X GET \
		"$(ES)/$(INDEX)/_search?pretty"


