(ns puppetlabs.pcp.message-test
  (:require [clojure.test :refer :all]
            [puppetlabs.pcp.message :refer :all]
            [puppetlabs.kitchensink.core :as ks]
            [schema.core :as s]
            [slingshot.test]))

(deftest make-message-test
  (testing "it makes a message"
    (is (= {:sender ""
            :targets []
            :expires "1970-01-01T00:00:00.000Z"
            :message_type ""}
           (dissoc (make-message) :_chunks :id))))
  (testing "it makes a message with parameters"
    (let [message (make-message :sender "pcp://client01.example.com/test"
                                :targets ["pcp:///server"])]
      (is (= {:sender "pcp://client01.example.com/test"
              :targets ["pcp:///server"]
              :expires "1970-01-01T00:00:00.000Z"
              :message_type ""}
             (dissoc message :_chunks :id))))))

(deftest set-expiry-test
  (testing "it sets expiries to what you tell it"
    (is (= (:expires (set-expiry (make-message) "1971-01-01T00:00:00.000Z")) "1971-01-01T00:00:00.000Z")))
  (testing "it supports relative time"
    ;; Hello future test debugger.  At one point someone said "we
    ;; should never be 3 seconds before the epoch".  Past test writer
    ;; needs a slap.
    (is (not (= (:expires (set-expiry (make-message) 3 :seconds)) "1970-01-01T00:00:00.000Z")))))

(deftest get-data-test
  (testing "it returns data from the data frame"
    (let [message (set-data (make-message) (byte-array [4 6 2]))]
      (is (= [4 6 2]
             (vec (get-data message)))))))

(deftest set-data-test
  (testing "it sets the data frame"
    (let [message (set-data (make-message) (byte-array [1 2 3]))]
      (is (= (vec (get-data message))
             [1 2 3])))))

(deftest get-json-data-test
  (testing "it json decodes the data frame"
    (let [message (set-data (make-message) (string->bytes "{}"))]
      (is (= (get-json-data message) {})))))

(deftest set-json-data-test
  (testing "it json encodes to the data frame"
    (let [message (set-json-data (make-message) {})]
      (is (= (bytes->string (get-data message))
             "{}")))))

(deftest encode-descriptor-test
  (testing "it encodes"
    (is (= 1
           (encode-descriptor {:type 1})))
    (is (= 2r10000001
           (encode-descriptor {:type 1 :flags #{:unused1}})))
    (is (= 2r10010001
           (encode-descriptor {:type 1 :flags #{:unused1 :unused4}})))))

(deftest decode-descriptor-test
  (testing "it decodes"
    (is (= {:type 1 :flags #{}}
           (decode-descriptor 1)))
    (is (= {:type 1 :flags #{:unused1}}
           (decode-descriptor 2r10000001)))
    (is (= {:type 1 :flags #{:unused1 :unused4}}
           (decode-descriptor 2r10010001)))))

(deftest encode-test
  (testing "when being strict, we take a Message only"
    (s/with-fn-validation
      (is (thrown+? [:type :schema.core/error]
                    (encode {}))
          "Rejected an empty map as a Message")))
  (with-redefs [message->envelope (constantly {})]
    (testing "it returns a byte array"
      ;; subsequent tests will use vec to ignore this
      (is (= (class (byte-array 0))
             (class (encode (make-message))))))
    (testing "it encodes a message"
      (is (= [1,
              1, 0 0 0 2, 123 125,
              2, 0 0 0 0]
             (vec (encode (make-message))))))
    (testing "it adds debug type as an optional final chunk"
      (is (= [1,
              1, 0 0 0 2, 123 125,
              2, 0 0 0 0,
              3, 0 0 0 4, 115 111 109 101]
             (vec (encode (set-debug (make-message) (string->bytes "some")))))))
    (testing "it encodes the data chunk"
      (is (= [1,
              1, 0 0 0 2, 123 125,
              2, 0 0 0 4, 104 97 104 97]
             (vec (encode (set-data (make-message) (string->bytes "haha")))))))))

(deftest decode-test
  (with-redefs [schema.core/validate (fn [s d] d)]
    (testing "it only handles version 1 messages"
      (is (thrown+? [:type :puppetlabs.pcp.message/message-malformed]
                    (decode (byte-array [2])))))
    (testing "it insists on envelope chunk first"
      (is (thrown+? [:type :puppetlabs.pcp.message/message-invalid]
                    (decode (byte-array [1,
                                         2, 0 0 0 2, 123 125])))))
    (testing "it decodes the null message"
      (is (= (dissoc (message->envelope (make-message)) :id)
             (dissoc (message->envelope (decode (byte-array [1, 1, 0 0 0 2, 123 125]))) :id))))
    (testing "it insists on a well-formed envelope"
      (is (thrown+? [:type :puppetlabs.pcp.message/envelope-malformed]
                    (decode (byte-array [1,
                                         1, 0 0 0 1, 123])))))
    (testing "it insists on a complete envelope"
      (with-redefs [schema.core/validate (fn [s d] (throw (Exception. "oh dear")))]
        (is (thrown+? [:type :puppetlabs.pcp.message/envelope-invalid]
                      (decode (byte-array [1,
                                           1, 0 0 0 2, 123 125]))))))
    (testing "data is accessible"
      (let [message (decode (byte-array [1,
                                         1, 0 0 0 2, 123 125,
                                         2, 0 0 0 3, 108 111 108]))]
        (is (= (String. (get-data message)) "lol"))))
    (testing "debug is accessible"
      (let [message (decode (byte-array [1,
                                         1, 0 0 0 2, 123 125,
                                         2, 0 0 0 0,
                                         3, 0 0 0 3, 108 111 108]))]
        (is (= "lol" (String. (get-debug message))))))))

(deftest encoder-roundtrip-test
  (with-redefs [schema.core/validate (fn [s d] d)]
    (testing "it can roundtrip data"
      (let [data (byte-array (map byte "hola"))
            encoded (encode (set-data (make-message) data))
            decoded (decode encoded)]
        (is (= (vec (get-data decoded))
               (vec data)))))))
