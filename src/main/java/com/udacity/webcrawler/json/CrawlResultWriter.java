package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;


public final class CrawlResultWriter {
  private final CrawlResult result;

  
  public CrawlResultWriter(CrawlResult result) {
    this.result = Objects.requireNonNull(result);
  }

  
  public void write(Path path) {
    Objects.requireNonNull(path);

    try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, CREATE, APPEND)) {
      write(writer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  
  public void write(Writer writer) {
    Objects.requireNonNull(writer);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    try {
      objectMapper.writeValue(writer, result);
      writer.write("\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
