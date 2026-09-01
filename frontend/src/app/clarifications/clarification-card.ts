import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

import { ClarificationResponse } from '../api';
import { Markdown } from '../shared/markdown.component';
import { AnswerForm } from './answer-form';

/** R22. One question, with its answer form while it is open and its answer once it is not. */
@Component({
  selector: 'pd-clarification-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatCardModule, MatIconModule, AnswerForm, Markdown],
  templateUrl: './clarification-card.html',
  styleUrl: './clarification-card.scss',
})
export class ClarificationCard {
  public readonly clarification = input.required<ClarificationResponse>();
  public readonly answered = output<ClarificationResponse>();
}
