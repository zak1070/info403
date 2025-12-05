; Target: LLVM IR
declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
@.str_out = private unnamed_addr constant [4 x i8] c"%d\0A\00"
@.str_in = private unnamed_addr constant [3 x i8] c"%d\00"
@.str_prompt = private unnamed_addr constant [9 x i8] c"input : \00"

define i32 @main() {
entry:
  ; Variable allocations
  %a = alloca i32
  %b = alloca i32
  %c = alloca i32
  ; End allocations

  %1 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([9 x i8], [9 x i8]* @.str_prompt, i32 0, i32 0))
  %2 = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str_in, i32 0, i32 0), i32* %a)
  %3 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([9 x i8], [9 x i8]* @.str_prompt, i32 0, i32 0))
  %4 = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str_in, i32 0, i32 0), i32* %b)
  br label %label_1
label_1:
  %5 = load i32, i32* %b
  %6 = icmp slt i32 0, %5
  br i1 %6, label %label_2, label %label_3
label_2:
  %7 = load i32, i32* %b
  store i32 %7, i32* %c
  br label %label_4
label_4:
  %8 = load i32, i32* %b
  %9 = load i32, i32* %a
  %10 = icmp sle i32 %8, %9
  br i1 %10, label %label_5, label %label_6
label_5:
  %11 = load i32, i32* %a
  %12 = load i32, i32* %b
  %13 = sub i32 %11, %12
  store i32 %13, i32* %a
  br label %label_4
label_6:
  %14 = load i32, i32* %a
  store i32 %14, i32* %b
  %15 = load i32, i32* %c
  store i32 %15, i32* %a
  br label %label_1
label_3:
  %16 = load i32, i32* %a
  %17 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str_out, i32 0, i32 0), i32 %16)
  ret i32 0
}
