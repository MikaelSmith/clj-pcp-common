(ns puppetlabs.pcp.protocol-v2
  (:require [puppetlabs.pcp.protocol-v1 :as v1]
            [schema.core :as s]))

(def ISO8601 v1/ISO8601)

(def Uri v1/Uri)

(def uuid? v1/uuid?)

(def MessageId v1/MessageId)

(def Envelope
  "Defines the envelope format of a v2 message"
  {:id           MessageId
   (s/optional-key :in-reply-to) MessageId
   :sender       Uri
   :targets      [Uri]
   :message_type s/Str
   :expires      ISO8601
   (s/optional-key :destination_report) s/Bool})

(def AssociateResponse v1/AssociateResponse)

(def InventoryRequest v1/InventoryRequest)

(def InventoryResponse v1/InventoryResponse)

(def DestinationReport v1/DestinationReport)

(def ErrorMessage v1/ErrorMessage)

(def TTLExpiredMessage v1/TTLExpiredMessage)

(def VersionErrorMessage v1/VersionErrorMessage)

(def DebugChunk v1/DebugChunk)

(def explode-uri v1/explode-uri)

(def uri-wildcard? v1/uri-wildcard?)
