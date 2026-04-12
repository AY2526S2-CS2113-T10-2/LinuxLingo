# Test Report — Purav (Exam Module)

### invalid input in MCQ questions (eg. typing the answer instead of A, B, C, D or empty input) - FIXED

*expected*: error message prompting user to input a valid option (A, B, C, D)
*actual*: the answer is marked as incorrect, and the user receives feedback on the correct answer. No error message is shown for invalid input.

### No explanation provided for FITB and PRAC questions - FIXED

### inputting invalid topic while topic selection (eg. 8 or typo in topic name) - FIXED

*expected* error message prompting user to select a valid topic
*actual* command exits and you have to type in exam again

### Using quit on PRAC question does not work

Maybe it should be mentioned in the UG that quit only works for mcq and fitb

### using quit when on exam -random simply skips the question without any output like showing score 0/1 incorrect or skipped - FIXED

### exam -t navigation -t file-management (duplicate topics)

starts an exam with all question from the latest topic mentioned

*expected* error message prompting user to enter command in proper format

### exam command with unknown or empty flags (eg: exam -topic navigation or exam -t)

*expected* error message prompting user to enter command in proper format
*actual* exam session is started, prompting user to select topic and unknown flag is ignored




