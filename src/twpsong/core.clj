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

(def ^:dynamic *dubstepid* (:target (dubstep)))

(defn updatedub
   "update params for dubstep background"
   [params]

   (def strcnt (count (params "tweet")))
   (def cwobblerate 280) ;((node-get-control *dubstepid*) :wobble))
   (def wobrate
        (if
                (= (params "emotion") :happy) (- cwobblerate strcnt) (+ cwobblerate strcnt)
        )
        )
   (if (< 3 wobrate) (def wrate 3) (if (> 50 wobrate) (def wrate 50) nil))

   (ctl *dubstepid* :wobble wrate)
)

(defmulti play-sound
  (fn[x] (x "inst")))

(defmethod play-sound 3 [params];
        (if (= (params "emotion") :happy)
                (callpiano (params "tweet"))
                (callpiano "SSSSSS")

        )
)

(defmethod play-sound 1 [params]
        (twp/round-kick)
)

(defmethod play-sound 2 [params]
        (twp/rise-fall-pad)
)

(defmethod play-sound :default [params]
        (updatedub params)
)


(defn pick-and-play [tweet]
        (def instselect (rand-int 100))
        (def emot (compute-emotion (word-split tweet)))
        (def parammap {"inst" instselect, "tweet" tweet, "emotion" emot })
        (play-sound parammap)
)

(def ^:dynamic *creds* (make-oauth-creds "DWQeZVQiFG1V0abuu5yKw"
                             "EXFHdOtIIBzzW6W4l5Aq8tx94pRUFv3pjHGLTqx8l4"
                         "562448092-aranxSAuTHi7BXcB9OMq25iq09qroEC4yEUieKwB"
                         "TyIFXRr5RclyfDeic7wMmnZmrpcqSElZWWeOkehKqOY"))


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
