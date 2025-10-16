package com.example.jxlsstream.parquet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.LocalFileSystem;

/**
 * Creates Hadoop {@link Configuration} instances that gracefully fall back to the
 * local file system when the native Hadoop installation (HADOOP_HOME) is not
 * available. This keeps the sample runnable on developer machines without a
 * full Hadoop setup while still allowing real Hadoop configurations to take
 * effect when present.
 */
final class HadoopConfigurationFactory {

  private static final String HADOOP_HOME = System.getenv("HADOOP_HOME");

  private HadoopConfigurationFactory() {
  }

  static Configuration create() {
    Configuration conf = new Configuration();

    if (HADOOP_HOME == null || HADOOP_HOME.isBlank()) {
      // Force the file:// scheme and local implementations so paths resolve
      // against the developer's filesystem instead of expecting HDFS.
      conf.set("fs.defaultFS", "file:///");
      conf.set("mapreduce.framework.name", "local");
      conf.set("fs.file.impl", LocalFileSystem.class.getName());
      conf.setBoolean("fs.file.impl.disable.cache", true);

      // Some Hadoop components still look up this system property, so make sure
      // it points somewhere benign when no installation is present.
      System.setProperty("hadoop.home.dir", System.getProperty("user.dir"));
    }

    return conf;
  }
}
