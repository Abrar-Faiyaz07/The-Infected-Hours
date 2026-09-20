from pathlib import Path
from pypdf import PdfReader

source_dir = Path(r"E:\HP\drive-download-20260913T055318Z-1-001\all_lab_task_AfterMid")
output_dir = Path(r"D:\SaminWorks\01.CSE4402_Project\infected-hour-git\tmp\lab10_14_extract")
output_dir.mkdir(parents=True, exist_ok=True)

names = [
    "CSE 4404 Lab10 1A-1B.pdf",
    "CSE 4404 Lab10_2B.pdf",
    "CSE_4404_Lab_Handout_2026 (4).pdf",
    "CSE 4404 Lab 12 1A_1B.pdf",
    "CSE_4404 Lab 13 1A-1B.pdf",
    "CSE4404_Lab13_2A.pdf",
    "CSE4404_Lab13_2B.pdf",
    "CSE_4404 Lab 14 1A-1B_new.pdf",
    "CSE_4404 Lab 14 2B.pdf",
    "Lecture 10.1 Greedy2.pdf",
    "DSU.pdf",
    "Max Flow Lecture.pdf",
    "MaxFlow_Skeleton_flat.pdf",
]

for name in names:
    path = source_dir / name
    reader = PdfReader(path)
    pages = []
    for i, page in enumerate(reader.pages, start=1):
        pages.append(f"\n===== PAGE {i} =====\n{page.extract_text() or ''}")
    out = output_dir / f"{name}.txt"
    out.write_text("".join(pages), encoding="utf-8")
    print(f"{name}: {len(reader.pages)} pages, {out.stat().st_size} bytes")
