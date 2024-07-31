```yaml
enabled: true
serverGrayEnabled: true
grayscale:
  - consumerGroupTag: gray       # 灰度消费组标识
    envTag:                      # 灰度消费组对应环境标签，如果使用版本号，标签的key写'version'
      cas_lane_tag: gray
    trafficTag:                  # 灰度消费组对应流量标签
      x-cse-canary: gray
base:
  consumeType: auto
  autoCheckDelayTime: 30
```