(ns twpsong.core
  (:use
   [twitter.oauth]
   [twitter.callbacks]
   [twitter.callbacks.handlers]
   [twitter.api.restful]
   [twitter.api.streaming]
  )
  (:require
   [clojure.data.json :as json]
   [http.async.client :as ac]
  )
  (:import
   (twitter.callbacks.protocols AsyncStreamingCallback)
   (twitter.callbacks.protocols SyncSingleCallback))
)

(def ^:dynamic *creds* (make-oauth-creds "i6dB5Xjl4z2hw6kEUsu0Zg"
                             "AqoPKbPHurK9tJPMogvioPf5iwO7RQMXwWfsitlvg"
                         "251902819-Gji3Z2uVba0Luw7xHx6hJ3pFkghYXuGrWlNeKkXc"
                         "Vpe02tSHL73URmPkBgUFLeSFXPDhWvmmnFMafP6TKhY"))

(def ^:dynamic *response* (user-stream :oauth-creds *creds*))
	(Thread/sleep 60000)
	((:cancel (meta *response*)))
(def ^:dynamic 
     *custom-streaming-callback* 
     (AsyncStreamingCallback. (comp println #(:text %) json/read-json #(str %2)) 
                      (comp println response-return-everything)
                  exception-print))


;;  (show-user :oauth-creds *creds* :params {:screen-name "robelsner"})
;;  (statuses-filter :params {:track "Borat"}
;;         :oauth-creds *creds*
;;         :callbacks *custom-streaming-callback*)
