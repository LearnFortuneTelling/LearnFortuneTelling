# Ganji Engine

## 계산 기준
KST (UTC+9)
연주: 입춘 시각 기준
월주: 절입 시각 기준
일주: 00:00 변경
야자시: 23:00~23:59
조자시: 00:00~00:59
절기: 태양 겉보기 황경 기반 자체 계산

## 사용법

```ts
import {
  calculateFourPillars,
  createKstDate
} from "./ganji";

const date = createKstDate(
  2026,
  9,
  13,
  12,
  30
);

const result =
  calculateFourPillars(date);

console.log(result);
```

## 결과

```
{
  year: { ... },
  month: { ... },
  day: { ... },
  hour: { ... }
}
```