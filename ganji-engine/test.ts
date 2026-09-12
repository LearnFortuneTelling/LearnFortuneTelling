import {
  calculateFourPillars,
  createKstDate,
  getSolarTerm
} from "./index";

function assertEqual(
  actual: string,
  expected: string,
  label: string
) {
  if (actual !== expected) {
    throw new Error(
      `${label} 실패: expected=${expected}, actual=${actual}`
    );
  }

  console.log(`PASS: ${label}`);
}


// ----------------------------------
// 일반 날짜
// ----------------------------------

const normal = calculateFourPillars(
  createKstDate(2026, 9, 10, 21, 30)
);

assertEqual(normal.year.name, "병오", "일반 날짜 연주");
assertEqual(normal.month.name, "정유", "일반 날짜 월주");
assertEqual(normal.day.name, "정해", "일반 날짜 일주");
assertEqual(normal.hour.name, "신해", "일반 날짜 시주");


// ----------------------------------
// 야자시 / 조자시
// ----------------------------------

const nightRat = calculateFourPillars(
  createKstDate(2026, 9, 10, 23, 30)
);

const morningRat = calculateFourPillars(
  createKstDate(2026, 9, 11, 0, 30)
);

assertEqual(nightRat.day.name, "정해", "야자시 일주");
assertEqual(nightRat.hour.name, "임자", "야자시 시주");

assertEqual(morningRat.day.name, "무자", "조자시 일주");
assertEqual(morningRat.hour.name, "임자", "조자시 시주");


// ----------------------------------
// 입춘 경계
// ----------------------------------

const ipchun = getSolarTerm(2026, "입춘");

const beforeIpchun = calculateFourPillars(
  new Date(ipchun.getTime() - 1000)
);

const afterIpchun = calculateFourPillars(
  new Date(ipchun.getTime() + 1000)
);

assertEqual(beforeIpchun.year.name, "을사", "입춘 직전 연주");
assertEqual(beforeIpchun.month.name, "기축", "입춘 직전 월주");

assertEqual(afterIpchun.year.name, "병오", "입춘 직후 연주");
assertEqual(afterIpchun.month.name, "경인", "입춘 직후 월주");


// ----------------------------------
// 경칩 경계
// ----------------------------------

const gyeongchip = getSolarTerm(2026, "경칩");

const beforeGyeongchip = calculateFourPillars(
  new Date(gyeongchip.getTime() - 1000)
);

const afterGyeongchip = calculateFourPillars(
  new Date(gyeongchip.getTime() + 1000)
);

// assertEqual(실제값, 기대값, 테스트이름);
assertEqual(beforeGyeongchip.year.name, "병오", "경칩 직전 연주");
assertEqual(beforeGyeongchip.month.name, "경인", "경칩 직전 월주");

assertEqual(afterGyeongchip.year.name, "병오", "경칩 직후 연주");
assertEqual(afterGyeongchip.month.name, "신묘", "경칩 직후 월주");


console.log("!!! 모든 테스트 통과 !!!");

/* 코드 파일 실행 커맨드
npx tsx test.ts
*/
