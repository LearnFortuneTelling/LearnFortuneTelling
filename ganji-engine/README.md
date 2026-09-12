# Ganji Engine

요약: 입력한 날짜 & 시간을 기준으로 해당 시점과 연도의 절기 시각을 태양 황경으로 계산하고, 기준일과 60갑자 순환 규칙을 적용해 년주, 월주, 일주, 시주를 계산한다

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