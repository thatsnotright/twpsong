(ns twpsong.core
  (:use 
   [overtone.core]
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

(boot-external-server)
(use 'overtone.inst.piano)


(defn callkick 
  "kick drum"
  [response content]
  (def tweet (:text (json/read-json (str content))))
  (def strcnt (count tweet))
;  (if (> strcnt 30) (if (< strcnt 35) (twp/twpkick) ))
   (play-notes (now) 320 piano 
	(map 
		#(nth notes %) 
		(map 
			#(int 
				(/ 
					(* 
						(- (int %) 32)
					 8)
				 94)
			 )
		  (seq tweet)
		)
	)
   )
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

(def ^:dynamic *creds* (make-oauth-creds "DWQeZVQiFG1V0abuu5yKw"
                             "EXFHdOtIIBzzW6W4l5Aq8tx94pRUFv3pjHGLTqx8l4"
                         "562448092-aranxSAuTHi7BXcB9OMq25iq09qroEC4yEUieKwB"
                         "TyIFXRr5RclyfDeic7wMmnZmrpcqSElZWWeOkehKqOY"))

(defn -main [& args]
	(Thread/sleep 1000)
	(twp/twpkick)
;	(def ^:dynamic *dubstepid* (:target (dubstep)))
;	(def dubcallback (fn [response baos] (updatedub response baos *dubstepid*)));
;	(statuses-filter :params {:track "IADMIT"}
;	       :oauth-creds *creds*
;	       :callbacks (AsyncStreamingCallback. dubcallback (comp println response-return-everything)
;                 exception-print))

	(statuses-filter :params {:track "twpsong"}
	       :oauth-creds *creds*
	       :callbacks *kicker-callback*)

	(Thread/sleep 60000)
	(stop)
	(System/exit 0)
)
