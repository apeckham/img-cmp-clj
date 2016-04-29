FROM clojure
RUN apt-get update && apt-get install imagemagick -y && rm -rf /var/lib/apt/lists/*
COPY . /cmp
WORKDIR /cmp
RUN lein uberjar && cp target/uberjar/img-cmp-clj-standalone.jar /
CMD ["java", "-jar", "/img-cmp-clj-standalone.jar"]
