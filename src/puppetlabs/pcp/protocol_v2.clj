(ns puppetlabs.pcp.protocol-v2
  (:require [clojure.string :as str]
            [puppetlabs.kitchensink.core :as ks]
            [schema.core :as s]))

(def ISO8601
  "Schema validates if string conforms to ISO8601"
  (s/pred ks/datetime? 'datetime?))

(def Uri
  "Schema for PCP node Uri"
  (s/pred (partial re-matches #"^pcp://[^/]*/[^/]+$") 'uri?))

(defn uuid?
  [uuid]
  (re-matches #"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$" uuid))

(def MessageId
  "A message identifier"
  (s/pred uuid?))

(def Envelope
  "Defines the envelope format of a v2 message"
  {:id           MessageId
   (s/optional-key :in-reply-to) MessageId
   :sender       Uri
   :targets      [Uri]
   :message_type s/Str
   :ttl          s/Int ;; in milliseconds
   (s/optional-key :destination_report) s/Bool})

(def AssociateResponse
  "Schema for http://puppetlabs.com/associate_response"
  {:id MessageId
   :success s/Bool
   (s/optional-key :reason) s/Str})

(def InventoryRequest
  "Data schema for http://puppetlabs.com/inventory_request"
  {:query [Uri]})

(def InventoryResponse
  "Data schema for http://puppetlabs.com/inventory_response"
  {:uris [Uri]})

(def DestinationReport
  "Defines the data field for a destination report body"
  {:id MessageId
   :targets [Uri]})

(def ErrorMessage
  "Data schema for http://puppetlabs.com/error_message"
  {(s/optional-key :id) MessageId
   :description s/Str})

(def TTLExpiredMessage
  "Data schema for http://puppetlabs.com/ttl_expired"
  {:id MessageId})

(def VersionErrorMessage
  "Data schema for http://puppetlabs.com/version_error"
  {:id MessageId
   :target s/Str
   :reason s/Str})

(def DebugChunk
  "Data schema for a debug chunk"
  {:hops [{(s/required-key :server) Uri
           (s/optional-key :stage) s/Str
           (s/required-key :time) ISO8601}]})

(s/defn explode-uri :- [s/Str]
  "Parse an Uri string into its component parts.  Raises if incomplete"
  [uri :- Uri]
  (str/split (subs uri 6) #"/"))

(s/defn uri-wildcard? :- s/Bool
  [uri :- Uri]
  (let [chunks (explode-uri uri)]
    (some? (some (partial = "*") chunks))))
