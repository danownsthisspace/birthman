(ns app
  (:require ["inquirer$default" :as inquirer]
            ["moment$default" :as moment]
            ["mongodb$default" :as mongodb]
            [nbb.core :refer [await]]
            [promesa.core :as p]))


(def password js/process.env.BMAN_DB_PASS)
(def user (or js/process.env.BMAN_DB_USER "nbb"))
(def uri (str "mongodb+srv://" user ":" password "@cluster0.2pbdq.mongodb.net/birthdaydb?retryWrites=true&w=majority"))

(def client (new mongodb/MongoClient uri))

(defn write-birthday [name day month]
  (p/let [conn (.connect client)
          db (.db conn "birthdaydb")
          collection (.collection db "birthdays")
          response (.insertOne collection #js {:name name
                                               :day (str day)
                                               :month month})
          _ (.close conn)]
    response))

(comment
  (await (write-birthday "Tom" 18 "January")))

(def questions  (clj->js [{:name "name"
                           :type "input"
                           :message "Who's birthday is it?"}
                          {:name "day"
                           :type "number"
                           :message "What day is their birthday?"
                           :validate (fn [v]
                                       (<= 1 v 31))}
                          {:name "month"
                           :type "list"
                           :choices (moment/months)}]))


(defn create-birthday-entry []
  (p/let [_answers (inquirer/prompt questions)
          answers (js->clj _answers :keywordize-keys true)
          {:keys [name day month]} answers]
    (prn "Saving Birthday for" name day month)
    (write-birthday name day month)))

(defn find-birthday-entries [day month]
  (println "Finding birthdays" day month)
  (p/let [query #js {:month month
                     :day (str day)}
          conn (.connect client)
          db (.db conn "birthdaydb")
          collection (.collection db "birthdays")
          response (.toArray (.find collection query))
          _ (.close conn)]
    response))

(comment
  (await (find-birthday-entries 1 "January"))
  (await (find-birthday-entries 7 "April")))


(defn list-birthdays []
  (p/let [month (.format (moment) "MMMM")
          day (.date (moment))
          _entries (find-birthday-entries day month)
          entries (js->clj _entries :keywordize-keys true)]
    (run! (fn [{:keys [name]}]
           (println "It's" (str name "'s") "birtday today 🎂")) entries)))

(comment
  (await (list-birthdays))
  )

(cond
  (= (first *command-line-args*) "list") (list-birthdays)
  :else (create-birthday-entry))

