
package com.wyzssw.distributedLock;

import org.apache.commons.lang3.StringUtils;

import redis.clients.jedis.Jedis;

/**
 * redis实现的distributedlock ,锁占用时间不宜过长
 * @see http://www.jeffkit.info/2011/07/1000/
 * @author wyzssw
 */
public class DistributedLock {
    private String host;
    
    private Integer port;
    
    //单位毫秒
    private long lockTimeOut;
    
    
    /**
     * @return the lockTimeOut
     */
    public long getLockTimeOut() {
        return lockTimeOut;
    }

    private long perSleep;
    
    /**
     * 得不到锁立即返回，得到锁返回设置的超时时间
     * @param key
     * @return
     */
    public long tryLock(String key){
        //得到锁后设置的过期时间，未得到锁返回0
        long expireTime = 0;
        Jedis jedis = null;
        jedis = new Jedis(host, port);
        expireTime = System.currentTimeMillis() + lockTimeOut +1;
        if (jedis.setnx(key, String.valueOf(expireTime)) == 1) {
            //得到了锁返回
            return expireTime;
        }else  {
           String curLockTimeStr =  jedis.get(key);
           //判断是否过期
            if (StringUtils.isBlank(curLockTimeStr)
                || System.currentTimeMillis() > Long.valueOf(curLockTimeStr)) {
                expireTime = System.currentTimeMillis() + lockTimeOut +1;
                
                curLockTimeStr = jedis.getSet(key, String.valueOf(expireTime));
                //仍然过期,则得到锁
                if (StringUtils.isBlank(curLockTimeStr)
                        || System.currentTimeMillis() > Long.valueOf(curLockTimeStr)){
                    return expireTime;
                }else {
                    return 0;
                }
            }else {
                return 0;
            }
        }
    }
    
    /**
     * 得到锁返回设置的超时时间，得不到锁等待
     * @param key
     * @return
     * @throws InterruptedException
     */
    public long lock(String key) throws InterruptedException{
        long starttime = System.currentTimeMillis();
        long sleep = (perSleep==0?lockTimeOut/10:perSleep);
        //得到锁后设置的过期时间，未得到锁返回0
        long expireTime = 0;
        Jedis jedis = new Jedis(host, port);
        for (;;) {
        expireTime = System.currentTimeMillis() + lockTimeOut +1;
        if (jedis.setnx(key, String.valueOf(expireTime)) == 1) {
            //得到了锁返回
            return expireTime;
        }else  {
           String curLockTimeStr =  jedis.get(key);
           //判断是否过期
            if (StringUtils.isBlank(curLockTimeStr)
                || System.currentTimeMillis() > Long.valueOf(curLockTimeStr)) {
                expireTime = System.currentTimeMillis() + lockTimeOut +1;
                
                curLockTimeStr = jedis.getSet(key, String.valueOf(expireTime));
                //仍然过期,则得到锁
                if (StringUtils.isBlank(curLockTimeStr)
                        || System.currentTimeMillis() > Long.valueOf(curLockTimeStr)){
                    return expireTime;
                }else {
                    Thread.sleep(sleep);
                }
            }else {
                Thread.sleep(sleep);
            }
        }
		if (lockTimeOut > 0
					&& ((System.currentTimeMillis() - starttime) >= lockTimeOut)) {
				expireTime = 0;
				return expireTime;
			}             
        }
        
    }
    
    /**
     * 先判断自己运行时间是否超过了锁设置时间，是则不用解锁
     * @param key
     * @param expireTime
     */
    public void unlock(String key,long expireTime){
        if (System.currentTimeMillis()-expireTime>0) {
            return ;
        }
        Jedis jedis = new Jedis(host, port);
        String curLockTimeStr = jedis.get(key);
        if (StringUtils.isNotBlank(curLockTimeStr)&&Long.valueOf(curLockTimeStr)>System.currentTimeMillis()) {
            jedis.del(key);
        }
    }
    

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }
    
    /**
     * @param port the port to set
     */
    public void setPort(Integer port) {
        this.port = port;
    }
    
    
    /**
     * @param lockTimeOut the lockTimeOut to set
     */
    public void setLockTimeOut(long lockTimeOut) {
        this.lockTimeOut = lockTimeOut;
    }
    
    /**
     * @param perSleep the perSleep to set
     */
    public void setPerSleep(long perSleep) {
        this.perSleep = perSleep;
    }    
}
