package org.baeldung.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.time.DateUtils;
import org.baeldung.persistence.dao.PostRepository;
import org.baeldung.persistence.model.Post;
import org.baeldung.service.IScheduledPostService;
import org.baeldung.service.IUserService;
import org.baeldung.web.PagingInfo;
import org.baeldung.web.SimplePost;
import org.baeldung.web.exceptions.InvalidDateException;
import org.baeldung.web.exceptions.InvalidResubmitOptionsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ScheduledPostService implements IScheduledPostService {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final int LIMIT_SCHEDULED_POSTS_PER_DAY = 3;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private IUserService userService;

    //

    @Override
    public List<SimplePost> getPostsList(final int page, final int size, final String sortDir, final String sort) {
        final PageRequest pageReq = new PageRequest(page, size, Sort.Direction.fromString(sortDir), sort);
        final Page<Post> posts = postRepository.findByUser(userService.getCurrentUser(), pageReq);
        return constructDataAccordingToUserTimezone(posts.getContent());
    }

    @Override
    public PagingInfo generatePagingInfo(final int page, final int size) {
        final long total = postRepository.countByUser(userService.getCurrentUser());
        return new PagingInfo(page, size, total);
    }

    @Override
    public Post schedulePost(final boolean isSuperUser, final Post post, final String dateStr) throws ParseException {
        final Date submissionDate = calculateSubmissionDate(dateStr, userService.getCurrentUser().getPreference().getTimezone());
        if (!checkIfValidResubmitOptions(post)) {
            throw new InvalidResubmitOptionsException("Invalid Resubmit options");
        }
        if (submissionDate.before(new Date())) {
            throw new InvalidDateException("Scheduling Date already passed");
        }
        if (!(isSuperUser || checkIfCanSchedule(submissionDate))) {
            throw new InvalidDateException("Scheduling Date exceeds daily limit");
        }
        post.setSubmissionDate(submissionDate);
        post.setUser(userService.getCurrentUser());
        post.setSubmissionResponse("Not sent yet");
        return postRepository.save(post);
    }

    @Override
    public void updatePost(final boolean isSuperUser, final Post post, final String dateStr) throws ParseException {
        final Date submissionDate = calculateSubmissionDate(dateStr, userService.getCurrentUser().getPreference().getTimezone());
        if (!checkIfValidResubmitOptions(post)) {
            throw new InvalidResubmitOptionsException("Invalid Resubmit options");
        }
        if (submissionDate.before(new Date())) {
            throw new InvalidDateException("Scheduling Date already passed");
        }
        if (!(isSuperUser || checkIfCanSchedule(submissionDate))) {
            throw new InvalidDateException("Scheduling Date exceeds daily limit");
        }
        post.setSubmissionDate(submissionDate);
        post.setUser(userService.getCurrentUser());
        postRepository.save(post);
    }

    @Override
    public Post getPostById(final Long id) {
        return postRepository.findOne(id);
    }

    @Override
    public void deletePostById(final Long id) {
        postRepository.delete(id);
    }

    //

    private boolean checkIfValidResubmitOptions(final Post post) {
        final boolean isResubmitActivated = !checkIfAllEqualZero(post.getNoOfAttempts(), post.getTimeInterval(), post.getMinScoreRequired(), post.getMinTotalVotes()) || post.isKeepIfHasComments() || post.isDeleteAfterLastAttempt();

        if (isResubmitActivated) {
            if (checkIfAllNonZero(post.getNoOfAttempts(), post.getTimeInterval(), post.getMinScoreRequired())) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean checkIfAllEqualZero(final int... args) {
        for (final int tmp : args) {
            if (tmp != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkIfAllNonZero(final int... args) {
        for (final int tmp : args) {
            if (tmp == 0) {
                return false;
            }
        }
        return true;
    }

    private String convertToUserTomeZone(final Date date, final String timeZone) {
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        return dateFormat.format(date);
    }

    private List<SimplePost> constructDataAccordingToUserTimezone(final List<Post> posts) {
        final List<SimplePost> data = new ArrayList<SimplePost>(posts.size());
        final String timeZone = userService.getCurrentUser().getPreference().getTimezone();
        String date;
        for (final Post post : posts) {
            date = convertToUserTomeZone(post.getSubmissionDate(), timeZone);
            data.add(new SimplePost(post.getId(), post.getTitle(), date, post.getSubmissionResponse(), post.getNoOfAttempts()));
        }
        return data;
    }

    private synchronized final Date calculateSubmissionDate(final String dateString, final String timeZone) throws ParseException {
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        return dateFormat.parse(dateString);
    }

    private boolean checkIfCanSchedule(final Date date) {
        final Date start = DateUtils.truncate(date, Calendar.DATE);
        final Date end = DateUtils.addDays(start, 1);
        final long count = postRepository.countByUserAndSubmissionDateBetween(userService.getCurrentUser(), start, end);
        return count < LIMIT_SCHEDULED_POSTS_PER_DAY;
    }

}