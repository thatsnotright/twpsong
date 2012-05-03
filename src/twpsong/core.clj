(ns twpsong.core
  (:use 
   [overtone.core]
   [twpsong.sound :as twp]
   [twpsong.feeling]
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


(use 'overtone.inst.piano)

(def ^:dynamic pianodelay 320)

(def ^:dynamic drumbpm 120)

(defn callpiano 
  "play some piano notes based on a tweet"
  [content]
  (def tweet (str content)); (:text (json/read-json (str content))))
  (def strcnt (count tweet))
;  (if (> strcnt 30) (if (< strcnt 35) (twp/twpkick) ))
   (play-notes (now) pianodelay piano 
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


(defmulti play-sound
  (fn[x] (x "inst")))

(defmethod play-sound 3 [params];
	(callpiano (params "tweet"))
)

(defmethod play-sound 1 [params]
	(twp/round-kick)
)

(defmethod play-sound 2 [params]
	(twp/rise-fall-pad)
)

(defmethod play-sound :default [params]
;	(println (compute-emotion (word-split (params "tweet"))))	
;	(println "no sound")
	(updatedub (params "tweet"))
)


(defn pick-and-play [tweet]
	(def instselect (rand-int 100))
	(def parammap {"inst" instselect, "tweet" tweet})
	(play-sound parammap)
)

(def ^:dynamic *creds* (make-oauth-creds "DWQeZVQiFG1V0abuu5yKw"
                             "EXFHdOtIIBzzW6W4l5Aq8tx94pRUFv3pjHGLTqx8l4"
                         "562448092-aranxSAuTHi7BXcB9OMq25iq09qroEC4yEUieKwB"
                         "TyIFXRr5RclyfDeic7wMmnZmrpcqSElZWWeOkehKqOY"))

(def ^:dynamic *dubstepid* (:target (dubstep)))

(defn updatedub
   "update params for dubstep background"
   [content]
   
   (def strcnt (count content))
   (ctl *dubstepid* :wobble (/ (+ strcnt 1) 10))
)
(defn restart-dub-callback
	[response content]
	(updatedub response content)

)

(defn decider-callback
	[response content]
	(def tweet (str content)); (:text (json/read-json (str content))))
	(def strcnt (count tweet))

	(pick-and-play tweet)
)


(defn -main [& args]
	(Thread/sleep 1000)
	(twp/twpkick)

	(statuses-sample 
	       :oauth-creds *creds*
	       :callbacks (AsyncStreamingCallback. decider-callback (comp println response-return-everything)
                  exception-print))

	(Thread/sleep 60000)
	(stop)
	(System/exit 0)
)
