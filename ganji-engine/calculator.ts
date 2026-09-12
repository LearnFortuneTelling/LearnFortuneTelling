type SolarTermResult = {
  name: string;
  longitude: number;
  time: Date;
};

export type GanjiResult = {
  stemIndex: number;
  branchIndex: number;
  stem: string;
  branch: string;
  name: string;
};

export type FourPillarsResult = {
  year: GanjiResult;
  month: GanjiResult;
  day: GanjiResult;
  hour: GanjiResult;
};

const solarTermsCache = new Map<number, SolarTermResult[]>();

import {
  HEAVENLY_STEMS,
  EARTHLY_BRANCHES,
  SOLAR_TERMS
} from "./data";

const DAY_MS = 86_400_000;
const KST_OFFSET_MS = 9 * 60 * 60 * 1000;

// 2000-01-01은 무오(戊午)일.
// 갑자를 0으로 잡으면 무오는 54번 인덱스.
const BASE_DAY_INDEX = 54;
const BASE_DATE = new Date("2000-01-01T00:00:00+09:00");


// --------------------------------------------------
// 기본 유틸
// --------------------------------------------------

function normalizeAngle(angle: number): number {
  return ((angle % 360) + 360) % 360;
}

function floorMod(value: number, modulo: number): number {
  return ((value % modulo) + modulo) % modulo;
}

function toRadians(degrees: number): number {
  return degrees * Math.PI / 180;
}


// --------------------------------------------------
// 60갑자
// --------------------------------------------------

export function getGanji(index: number) {
  const normalized = floorMod(index, 60);

  return {
    index: normalized,
    stemIndex: normalized % 10,
    branchIndex: normalized % 12,

    stem: HEAVENLY_STEMS[normalized % 10],
    branch: EARTHLY_BRANCHES[normalized % 12],

    name:
      HEAVENLY_STEMS[normalized % 10] +
      EARTHLY_BRANCHES[normalized % 12]
  };
}


// --------------------------------------------------
// Julian Day
// --------------------------------------------------

export function toJulianDay(date: Date): number {
  return date.getTime() / DAY_MS + 2440587.5;
}

export function julianCentury(jd: number): number {
  return (jd - 2451545.0) / 36525;
}


// --------------------------------------------------
// 태양 위치 계산
// --------------------------------------------------

export function sunMeanLongitude(T: number): number {
  const longitude =
    280.46646 +
    36000.76983 * T +
    0.0003032 * T * T;

  return normalizeAngle(longitude);
}

export function sunMeanAnomaly(T: number): number {
  const anomaly =
    357.52911 +
    35999.05029 * T -
    0.0001537 * T * T;

  return normalizeAngle(anomaly);
}


// 지구의 타원 공전에 따른 중심차 보정
export function sunEquationOfCenter(
  T: number,
  M: number
): number {

  const Mrad = toRadians(M);

  return (
    (
      1.914602
      - 0.004817 * T
      - 0.000014 * T * T
    ) * Math.sin(Mrad)

    + (
      0.019993
      - 0.000101 * T
    ) * Math.sin(2 * Mrad)

    + 0.000289 * Math.sin(3 * Mrad)
  );
}


// 진황경
export function sunTrueLongitude(T: number): number {
  const L0 = sunMeanLongitude(T);
  const M = sunMeanAnomaly(T);
  const C = sunEquationOfCenter(T, M);

  return normalizeAngle(L0 + C);
}


// 겉보기 황경
export function sunApparentLongitude(T: number): number {
  const trueLongitude = sunTrueLongitude(T);

  const omega =
    125.04
    - 1934.136 * T;

  return normalizeAngle(
    trueLongitude
    - 0.00569
    - 0.00478 * Math.sin(toRadians(omega))
  );
}


// 날짜 → 태양 황경
export function sunLongitude(date: Date): number {
  const jd = toJulianDay(date);
  const T = julianCentury(jd);

  return sunApparentLongitude(T);
}


// --------------------------------------------------
// 절기 계산
// --------------------------------------------------

function angleDifference(
  current: number,
  target: number
): number {

  let diff = current - target;

  if (diff > 180) {
    diff -= 360;
  }

  if (diff < -180) {
    diff += 360;
  }

  return diff;
}

function findSolarTermAround(
  approximateTime: Date,
  targetLongitude: number
): Date {

  let rangeDays = 1;

  while (rangeDays <= 32) {

    const start = new Date(
      approximateTime.getTime()
      - rangeDays * DAY_MS
    );

    const end = new Date(
      approximateTime.getTime()
      + rangeDays * DAY_MS
    );

    const leftDiff = angleDifference(
      sunLongitude(start),
      targetLongitude
    );

    const rightDiff = angleDifference(
      sunLongitude(end),
      targetLongitude
    );

    // 목표 황경을 이 구간에서 통과한다면
    if (leftDiff <= 0 && rightDiff >= 0) {
      return findSolarTermTime(
        start,
        end,
        targetLongitude
      );
    }

    // 못 찾았으면 탐색 범위를 2배 확장
    rangeDays *= 2;
  }

  throw new Error(
    `Could not find solar term ${targetLongitude}°`
  );
}


// 특정 시간 범위 안에서
// 태양 황경이 targetLongitude가 되는 시각 탐색
function findSolarTermTime(
  start: Date,
  end: Date,
  targetLongitude: number
): Date {

  let left = start.getTime();
  let right = end.getTime();

  const leftDiff =
    angleDifference(
      sunLongitude(new Date(left)),
      targetLongitude
    );

  const rightDiff =
    angleDifference(
      sunLongitude(new Date(right)),
      targetLongitude
    );

  // 목표 황경이 탐색 범위에 없으면 오류
  if (!(leftDiff <= 0 && rightDiff >= 0)) {
    throw new Error(
      `Solar term ${targetLongitude}° is not inside search range`
    );
  }

  // 1초 이하까지 이분 탐색
  while (right - left > 1000) {

    const middle =
      Math.floor((left + right) / 2);

    const middleLongitude =
      sunLongitude(new Date(middle));

    const diff =
      angleDifference(
        middleLongitude,
        targetLongitude
      );

    if (diff < 0) {
      left = middle;
    } else {
      right = middle;
    }
  }

  return new Date(
    Math.floor((left + right) / 2)
  );
}


// --------------------------------------------------
// 절기 예상 날짜
//
// 실제 절기 시각을 저장하는 것이 아니라
// "검색을 어디서 시작할지" 알려주는 앵커.
// --------------------------------------------------
// --------------------------------------------------
// 특정 연도의 24절기 계산
// --------------------------------------------------


function buildSolarTerms(year: number): SolarTermResult[] {
  const startOfYear =
    new Date(`${year}-01-01T00:00:00+09:00`);

  const startLongitude =
    sunLongitude(startOfYear);

  const SUN_DEGREES_PER_DAY =
    360 / 365.2422;

  return SOLAR_TERMS.map(term => {
    const degreeDifference =
      normalizeAngle(
        term.longitude - startLongitude
      );

    const approximateDays =
      degreeDifference / SUN_DEGREES_PER_DAY;

    const approximateTime =
      new Date(
        startOfYear.getTime()
        + approximateDays * DAY_MS
      );

    const time =
      findSolarTermAround(
        approximateTime,
        term.longitude
      );

    return {
      name: term.name,
      longitude: term.longitude,
      time
    };
  });
}

export function getSolarTerms(year: number): SolarTermResult[] {
  const cached =
    solarTermsCache.get(year);

  if (cached) {
    return cached;
  }

  const terms =
    buildSolarTerms(year);

  solarTermsCache.set(
    year,
    terms
  );

  return terms;
}

export function getSolarTerm(
  year: number,
  name: string
): Date {

  const term =
    getSolarTerms(year)
      .find(term => term.name === name);

  if (!term) {
    throw new Error(
      `Unknown solar term: ${name}`
    );
  }

  return term.time;
}


// --------------------------------------------------
// KST 날짜 관련
// --------------------------------------------------

function getKstParts(date: Date) {

  const shifted =
    new Date(
      date.getTime()
      + KST_OFFSET_MS
    );

  return {
    year: shifted.getUTCFullYear(),
    month: shifted.getUTCMonth() + 1,
    day: shifted.getUTCDate(),
    hour: shifted.getUTCHours(),
    minute: shifted.getUTCMinutes(),
    second: shifted.getUTCSeconds()
  };
}


// 프론트에서 KST 날짜를 만들 때 사용 가능
export function createKstDate(
  year: number,
  month: number,
  day: number,
  hour = 0,
  minute = 0,
  second = 0
): Date {

  return new Date(
    Date.UTC(
      year,
      month - 1,
      day,
      hour - 9,
      minute,
      second
    )
  );
}


// --------------------------------------------------
// 일주
// --------------------------------------------------

function getKstDayNumber(date: Date): number {

  return Math.floor(
    (date.getTime() + KST_OFFSET_MS)
    / DAY_MS
  );
}


export function getDayGanji(date: Date) {

  const baseDay =
    getKstDayNumber(BASE_DATE);

  const currentDay =
    getKstDayNumber(date);

  const difference =
    currentDay - baseDay;

  return getGanji(
    BASE_DAY_INDEX + difference
  );
}


// --------------------------------------------------
// 연주
// --------------------------------------------------

function getYearGanji(date: Date) {

  const { year } =
    getKstParts(date);

  const ipchun =
    getSolarTerm(year, "입춘");

  // 입춘 이전이면 아직 전년도 간지년
  const effectiveYear =
    date.getTime() < ipchun.getTime()
      ? year - 1
      : year;

  // 1984 = 갑자년
  const index =
    floorMod(
      effectiveYear - 1984,
      60
    );

  return getGanji(index);
}


// --------------------------------------------------
// 월주
// --------------------------------------------------

// 명리 월의 경계가 되는 12개의 절(節)
//
// 寅 = 입춘
// 卯 = 경칩
// 辰 = 청명
// 巳 = 입하
// 午 = 망종
// 未 = 소서
// 申 = 입추
// 酉 = 백로
// 戌 = 한로
// 亥 = 입동
// 子 = 대설
// 丑 = 소한

const MONTH_BOUNDARIES = [
  { name: "입춘", monthIndex: 0 },
  { name: "경칩", monthIndex: 1 },
  { name: "청명", monthIndex: 2 },
  { name: "입하", monthIndex: 3 },
  { name: "망종", monthIndex: 4 },
  { name: "소서", monthIndex: 5 },
  { name: "입추", monthIndex: 6 },
  { name: "백로", monthIndex: 7 },
  { name: "한로", monthIndex: 8 },
  { name: "입동", monthIndex: 9 },
  { name: "대설", monthIndex: 10 }
] as const;


// 명리 월 index
//
// 0 = 寅
// 1 = 卯
// ...
// 10 = 子
// 11 = 丑

function getSolarMonthIndex(
  date: Date
): number {

  const { year } =
    getKstParts(date);

  const currentTerms =
    getSolarTerms(year);

  const previousDaxue =
    getSolarTerm(
      year - 1,
      "대설"
    );

  const xiaohan =
    getSolarTerm(
      year,
      "소한"
    );

  // 1월 초, 소한 이전은
  // 전년도 대설부터 이어진 子월
  if (date < xiaohan) {
    return 10;
  }

  // 소한 ~ 입춘 = 丑월
  const ipchun =
    getSolarTerm(
      year,
      "입춘"
    );

  if (date < ipchun) {
    return 11;
  }

  // 입춘 이후 각 절입을 확인
  let currentMonth = 0;

  for (const boundary of MONTH_BOUNDARIES) {

    const term =
      currentTerms.find(
        term =>
          term.name === boundary.name
      );

    if (
      term &&
      date >= term.time
    ) {
      currentMonth =
        boundary.monthIndex;
    }
  }

  // previousDaxue는 위 논리를 명확히 하기 위한 계산값.
  // 실제 비교는 1월 분기에서 충분하다.
  void previousDaxue;

  return currentMonth;
}


function getMonthGanji(
  date: Date
) {

  const yearGanji =
    getYearGanji(date);

  const monthIndex =
    getSolarMonthIndex(date);

  // 寅월의 지지 index는 2
  const branchIndex =
    (2 + monthIndex) % 12;

  /*
    오호둔(五虎遁)

    甲/己年 → 丙寅
    乙/庚年 → 戊寅
    丙/辛年 → 庚寅
    丁/壬年 → 壬寅
    戊/癸年 → 甲寅

    이를 index 공식으로 표현하면:
    yearStemIndex * 2 + 2 + monthIndex
  */

  const stemIndex =
    floorMod(
      yearGanji.stemIndex * 2
      + 2
      + monthIndex,
      10
    );

  return {
    stemIndex,
    branchIndex,

    stem:
      HEAVENLY_STEMS[stemIndex],

    branch:
      EARTHLY_BRANCHES[branchIndex],

    name:
      HEAVENLY_STEMS[stemIndex]
      + EARTHLY_BRANCHES[branchIndex]
  };
}


// --------------------------------------------------
// 시주
// --------------------------------------------------

function getHourGanji(
  date: Date
) {

  const { hour } =
    getKstParts(date);

  /*
    子 23:00 ~ 00:59
    丑 01:00 ~ 02:59
    寅 03:00 ~ 04:59
    ...
  */

  const branchIndex =
    Math.floor(
      (hour + 1) / 2
    ) % 12;


  /*
    LFT 야자시 규칙

    23:00 ~ 23:59:
    일주는 당일 유지.
    하지만 시주의 천간 계산에는
    다음 날의 일간을 사용.

    00:00 ~ 00:59:
    이미 새 날짜의 일주 사용.
  */

  let referenceDate = date;

  if (hour === 23) {
    referenceDate =
      new Date(
        date.getTime() + DAY_MS
      );
  }

  const dayGanji =
    getDayGanji(referenceDate);

  /*
    오서둔(五鼠遁)

    甲/己日 → 甲子
    乙/庚日 → 丙子
    丙/辛日 → 戊子
    丁/壬日 → 庚子
    戊/癸日 → 壬子
  */

  const stemIndex =
    floorMod(
      dayGanji.stemIndex * 2
      + branchIndex,
      10
    );

  return {
    stemIndex,
    branchIndex,

    stem:
      HEAVENLY_STEMS[stemIndex],

    branch:
      EARTHLY_BRANCHES[branchIndex],

    name:
      HEAVENLY_STEMS[stemIndex]
      + EARTHLY_BRANCHES[branchIndex]
  };
}


// --------------------------------------------------
// 최종 사주 계산
// --------------------------------------------------

export function calculateFourPillars(
  date: Date
): FourPillarsResult {
  return {
    year: getYearGanji(date),
    month: getMonthGanji(date),
    day: getDayGanji(date),
    hour: getHourGanji(date)
  };
}

