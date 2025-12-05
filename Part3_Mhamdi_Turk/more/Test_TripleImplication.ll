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
  ; End allocations

  %1 = icmp slt i32 1, 2
  %2 = icmp sle i32 3, 4
  %3 = icmp eq i32 5, 5
  %4 = xor i1 %2, 1
  %5 = or i1 %4, %3
  %6 = xor i1 %1, 1
  %7 = or i1 %6, %5
  br i1 %7, label %label_1, label %label_2
label_1:
  %8 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([9 x i8], [9 x i8]* @.str_prompt, i32 0, i32 0))
  %9 = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str_in, i32 0, i32 0), i32* %a)
  %10 = load i32, i32* %a
  %11 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str_out, i32 0, i32 0), i32 %10)
  br label %label_3
label_2:
  %12 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([9 x i8], [9 x i8]* @.str_prompt, i32 0, i32 0))
  %13 = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str_in, i32 0, i32 0), i32* %b)
  %14 = load i32, i32* %b
  %15 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str_out, i32 0, i32 0), i32 %14)
  br label %label_3
label_3:
  ret i32 0
}
