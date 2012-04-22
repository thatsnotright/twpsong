(ns twpsong.core
  (:use 
	[overtone.live]
	[twpsong.sound :as twp]
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

(defn callkick 
  "kick drum"
  [response baos]
  (twp/kick)
)

(defn updatedub
   "update params for dubstep background"
   [response baos dubstepid]
   (def strcnt (count (:text (json/read-json (str baos)))))
   (ctl dubstepid :wobble (/ (+ strcnt 1) 10))
)

(def ^:dynamic 
     *kicker-callback* 
     (AsyncStreamingCallback. callkick (comp println response-return-everything)
                  exception-print) )

(def ^:dynamic *dubstep-callback* 
     (AsyncStreamingCallback. updatedub (comp println response-return-everything)
                  exception-print) )

(def ^:dynamic *creds* (make-oauth-creds "i6dB5Xjl4z2hw6kEUsu0Zg"
                             "AqoPKbPHurK9tJPMogvioPf5iwO7RQMXwWfsitlvg"
                         "251902819-Gji3Z2uVba0Luw7xHx6hJ3pFkghYXuGrWlNeKkXc"
                         "Vpe02tSHL73URmPkBgUFLeSFXPDhWvmmnFMafP6TKhY"))

(defn -main [& args]
	(twp/kick)
	(def ^:dynamic *dubstepid* (:target (dubstep)))
	(statuses-filter :params {:track "Tello"}
	       :oauth-creds *creds*
	       :callbacks *kicker-callback*)
	(Thread/sleep 1000)
	(def dubcallback (fn [response baos] (updatedub response baos *dubstepid*)))
	(statuses-filter :params {:track "Tello"}
	       :oauth-creds *creds*
	       :callbacks (AsyncStreamingCallback. dubcallback (comp println response-return-everything)
                  exception-print))
	(Thread/sleep 60000)
	(stop)
	(System/exit 0)
)
