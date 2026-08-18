/**
 * 내부용 표시 띠. 모든 화면 최상단에 항상 고정 노출한다.
 *
 * 운영 콘솔과 업체 대시보드는 구조가 비슷해 혼동하기 쉽다. 운영자가 자기 콘솔인 줄 알고
 * 고객 설정을 바꾸는 사고를 막기 위한 장치이므로 어떤 화면에서도 숨기지 않는다
 * (admin-console-plan.md §2.1).
 */
export function InternalBar() {
  return (
    <div
      className="sticky top-0 z-60 py-1 text-center font-mono text-[10.5px] tracking-[0.16em] text-[#a8d8cc]"
      style={{
        background:
          "repeating-linear-gradient(45deg, #1c1c1f, #1c1c1f 9px, #26262b 9px, #26262b 18px)",
      }}
    >
      내부 운영 도구 · 고객에게 보이지 않습니다
    </div>
  );
}
