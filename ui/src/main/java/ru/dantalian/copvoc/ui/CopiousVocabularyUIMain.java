package ru.dantalian.copvoc.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

import ru.dantalian.copvoc.web.CopiousVocabularyWebMain;

@SpringBootApplication
@Service
public class CopiousVocabularyUIMain {

	public static void main(final String[] args) {
		SpringApplication.run(CopiousVocabularyWebMain.class, args);
	}

}
