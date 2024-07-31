```yaml
enabled: true
serverGrayEnabled: true
grayscale:
  - consumerGroupTag: gray       # 灰度消费组标识
    envTag:                      # 灰度消费组对应环境标签
      cas_lane_tag: gray
    trafficTag:                  # 灰度消费组对应流量标签
      x-cse-canary: gray
base:
  consumeType: auto
  autoCheckDelayTime: 30
```