all:
	./mvnw clean dependency:copy-dependencies install && \
	java -cp target/dependency/*:target/java-parse-identify-summarise-1.0.0.jar Application

evalsetup:
	python3 -m venv venv && \
	. venv/bin/activate && \
	pip3 install -U rouge-score evaluate transformers[torch] torch

eval:
	. venv/bin/activate && \
	python3 evaluation.py
