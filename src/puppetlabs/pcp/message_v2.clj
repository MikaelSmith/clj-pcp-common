(ns puppetlabs.pcp.message-v2
  (:require [org.clojars.smee.binary.core :as b]
            [puppetlabs.pcp.message-v1 :as v1]
            [puppetlabs.pcp.protocol-v2 :refer [Envelope ISO8601]]
            [schema.core :as s]))

(def Message
  "Defines a v2 message object"
  (merge Envelope
         {:sender s/Str
          :_chunks {s/Keyword s/Any}}))

(s/defn v2->v1 :- v1/Message
  "Convert v2 message to v1: strip in-reply-to for everything but inventory_response"
  [message :- Message]
  (if (= "http://puppetlabs.com/inventory_response" (:message_type message))
    message
    (dissoc message :in-reply-to)))

(s/defn v1->v2 :- Message
  [message :- v1/Message]
  message)

(def ByteArray v1/ByteArray)

(def FlagSet v1/FlagSet)

(def string->bytes v1/string->bytes)

(def bytes->string v1/bytes->string)

(def message->envelope v1/message->envelope)

(def set-expiry v1/set-expiry)

(def get-data v1/get-data)

(def get-debug v1/get-debug)

(def set-data v1/set-data)

(def set-debug v1/set-debug)

(def get-json-data v1/get-json-data)

(def get-json-debug v1/get-json-debug)

(def set-json-data v1/set-json-data)

(def set-json-debug v1/set-json-debug)

(def make-message v1/make-message)

(def flag-bits v1/flag-bits)

(def encode-descriptor v1/encode-descriptor)

(def decode-descriptor v1/decode-descriptor)

(def descriptor-codec v1/descriptor-codec)

(def chunk-codec v1/chunk-codec)

(def message-codec
  (b/ordered-map
    :version (b/constant :byte 2)
    :chunks (b/repeated chunk-codec)))

(def encode-impl v1/encode-impl)

(s/defn encode :- ByteArray
  [message :- Message]
  (encode-impl message message->envelope message-codec))

(def decode-impl v1/decode-impl)

(s/defn decode :- Message
  [bytes :- ByteArray]
  (decode-impl bytes Envelope make-message message-codec))

