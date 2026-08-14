import Image from "next/image";

/**
 * 강점 캐러셀의 탭별 사진 — 각 탭이 말하는 것을 실제로 보여주는 이미지(무료 라이선스,
 * Pexels)로 골랐다. 인물 사진은 얼굴이 아니라 손·노트북·문서에 초점을 맞춘 항공샷만
 * 썼다 — 이 탭들은 "우리 고객"이 아니라 "이런 역할이 이런 화면을 씁니다"를 말하는
 * 자리라, 실제 고객 인물 사진처럼 보이면 없는 후기를 만든 것과 같아진다
 * (`landing-strengths-carousel.tsx` 상단 주석 참조).
 */
export function LandingStrengthsImage({ index }: { index: number }) {
  const image = IMAGES[index] ?? IMAGES[0];
  return (
    <div className="absolute inset-0 overflow-hidden">
      <Image
        src={image.src}
        alt=""
        aria-hidden
        fill
        priority={index === 0}
        sizes="900px"
        className="object-cover"
        style={{ objectPosition: image.position ?? "center" }}
      />
      <div className="absolute inset-0 bg-gradient-to-r from-black/75 via-black/35 to-black/10" />
    </div>
  );
}

const IMAGES: { src: string; position?: string }[] = [
  { src: "/landing/strengths/audience.jpg" },
  { src: "/landing/strengths/code-editor.jpg" },
  { src: "/landing/strengths/learning.jpg" },
  { src: "/landing/strengths/pricing.jpg" },
  { src: "/landing/strengths/color-palette.jpg" },
  { src: "/landing/strengths/security.jpg", position: "85% center" },
];
