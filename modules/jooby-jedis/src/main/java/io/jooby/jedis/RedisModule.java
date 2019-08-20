package io.jooby.jedis;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.inject.Provider;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.typesafe.config.Config;

import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisModule implements Extension {

  /**
   * Database name.
   */
  private String name;

	public RedisModule(final String name) {
    this.name = requireNonNull(name, "A db property is required.");;
  }

	 public RedisModule() {
	   this("db");
	  }

  @Override
	public void install(Jooby application) throws Exception {
    ServiceRegistry registry = application.getServices();

    Environment env = application.getEnvironment();
    Config config = env.getConfig();
    GenericObjectPoolConfig poolConfig = poolConfig(config, name);
    int timeout = (int) config.getDuration("jedis.timeout", TimeUnit.MILLISECONDS);
    URI uri = URI.create(config.getString(name));
    JedisPool pool = new JedisPool(poolConfig, uri, timeout);
    registry.put(JedisPool.class, pool);
    
    Provider<Jedis> jedis = () -> pool.getResource();
    ServiceKey<Jedis> jedisKey = ServiceKey.key(Jedis.class, name);
    registry.put(jedisKey, jedis);
    
    RedisProvider redisProvider = new RedisProvider(pool, uri, poolConfig);
    application.onStarting(redisProvider);
    application.onStop(redisProvider);
	}
    
  GenericObjectPoolConfig poolConfig(final Config config, final String name) {
    Config poolConfig = config.getConfig("jedis.pool");
    String override = "jedis." + name;
    if (config.hasPath(override)) {
      poolConfig = config.getConfig(override).withFallback(poolConfig);
    }
    return poolConfig(poolConfig);
  }

  GenericObjectPoolConfig poolConfig(final Config config) {
    GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    poolConfig.setBlockWhenExhausted(config.getBoolean("blockWhenExhausted"));
    poolConfig.setEvictionPolicyClassName(config.getString("evictionPolicyClassName"));
    poolConfig.setJmxEnabled(config.getBoolean("jmxEnabled"));
    poolConfig.setJmxNamePrefix(config.getString("jmxNamePrefix"));
    poolConfig.setLifo(config.getBoolean("lifo"));
    poolConfig.setMaxIdle(config.getInt("maxIdle"));
    poolConfig.setMaxTotal(config.getInt("maxTotal"));
    poolConfig.setMaxWaitMillis(config.getDuration("maxWait", TimeUnit.MILLISECONDS));
    poolConfig.setMinEvictableIdleTimeMillis(config.getDuration("minEvictableIdle",
        TimeUnit.MILLISECONDS));
    poolConfig.setMinIdle(config.getInt("minIdle"));
    poolConfig.setNumTestsPerEvictionRun(config.getInt("numTestsPerEvictionRun"));
    poolConfig.setSoftMinEvictableIdleTimeMillis(
        config.getDuration("softMinEvictableIdle", TimeUnit.MILLISECONDS));
    poolConfig.setTestOnBorrow(config.getBoolean("testOnBorrow"));
    poolConfig.setTestOnReturn(config.getBoolean("testOnReturn"));
    poolConfig.setTestWhileIdle(config.getBoolean("testWhileIdle"));
    poolConfig.setTimeBetweenEvictionRunsMillis(config.getDuration("timeBetweenEvictionRuns",
        TimeUnit.MILLISECONDS));

    return poolConfig;
  }

}
