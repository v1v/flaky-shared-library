// Licensed to Elasticsearch B.V. under one or more contributor
// license agreements. See the NOTICE file distributed with
// this work for additional information regarding copyright
// ownership. Elasticsearch B.V. licenses this file to you under
// the Apache License, Version 2.0 (the "License"); you may
// not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

/**
  Send the JSON report file to Elastisearch.

  sendDataToElasticsearch(es: "https://ecs.example.com:9200", data: '{"field": "value"}')
*/
def call(Map args = [:]){
  def es = args.containsKey('es') ? args.es : error("sendDataToElasticsearch: Elasticsearch URL is not valid.")
  def data = args.containsKey('data') ? args.data : error("sendDataToElasticsearch: data is not valid.")
  def restCall = args.containsKey('restCall') ? args.restCall : "/ci-builds/_doc/"
  def contentType = args.containsKey('contentType') ? args.contentType : "application/json"
  def method = args.containsKey('method') ? args.method : "POST"

  echo "sendDataToElasticsearch: sending data..."
  return httpRequest(url: "${es}${restCall}",
                     method: "${method}",
                     headers: ["Content-Type": "${contentType}" ],
                     data: data.toString() + "\n")
}
